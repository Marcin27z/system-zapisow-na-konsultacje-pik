package com.example.pikkonsultacje.Service;

import com.example.pikkonsultacje.Dao.ConsultationDao;
import com.example.pikkonsultacje.Dao.UserDao;
import com.example.pikkonsultacje.Dto.ConsultationSearchForm;
import com.example.pikkonsultacje.Dto.UserClientInfo;
import com.example.pikkonsultacje.Entity.Consultation;
import com.example.pikkonsultacje.Entity.User;
import com.example.pikkonsultacje.Enum.Role;
import com.example.pikkonsultacje.Enum.Status;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConsultationService {

    private ConsultationDao consultationDao;
    private UserDao userDao;
    private JavaMailSender mailSender;

    @Autowired
    @Qualifier("getJavaMailSender")
    public void setMailSender(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }
    @Autowired
    ConsultationService(ConsultationDao consultationDao, UserDao userDao) {
        this.consultationDao = consultationDao;
        this.userDao = userDao;
    }

    public boolean reserveConsultation(String consultationId, String username) {
        Optional<Consultation> consultation = consultationDao.findConsultationById(consultationId);
        if (consultation.isPresent()) {
            Consultation con = consultation.get();
            Optional<User> user = userDao.findUserByUsername(username);
            if (user.isPresent()) {
                if (con.reserve(user.get())) {
                    consultationDao.updateConsultation(con);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean addConsultation(Consultation consultation, String username) {
        Optional<User> user = userDao.findUserByUsername(username);
        if (user.isPresent()) {
            if (user.get().getRole() != Role.TUTOR) {
                return false;
            }
            consultation.setTutor(new UserClientInfo(user.get()));
            if (date_enabled(consultation)){
                consultationDao.insertConsultation(consultation);
                return true;
            }
        }
        return false;
    }

    private boolean date_enabled(Consultation consultation) {
        ConsultationSearchForm consultationSearchForm = new ConsultationSearchForm();
        consultationSearchForm.setDateStart(consultation.getConsultationStartTime());
        consultationSearchForm.setDateEnd(consultation.getConsultationEndTime());
        List<Consultation> consultations = findConsultations(consultationSearchForm);
        return consultations.isEmpty();
    }

    public boolean acceptStudentConsultation(String consultationId, String username) {
        Optional<Consultation> consultation = consultationDao.findConsultationById(consultationId);
        if (consultation.isPresent()){
            Consultation cons = consultation.get();
            if (cons.getTutor().getUsername().equals(username) && cons.getStatus() == Status.CREATED_BY_STUDENT){
                cons.setStatus(Status.RESERVED);
                sendEmail(cons.getStudent(), "Twoja konsultacja\n " + cons.toString() + "\n została zaakceptowana przez wykładowcę.");
                consultationDao.updateConsultation(cons);
                return true;
            }
        }
        return false;
    }



    public boolean cancelConsultation(String consultationId, String username) {
        Optional<Consultation> consultation = consultationDao.findConsultationById(consultationId);
        if (consultation.isPresent()) {
            Consultation con = consultation.get();
            Optional<User> user = userDao.findUserByUsername(username);
            if (user.isPresent()) {
                if (user.get().getRole() == Role.STUDENT) {
                    con.free();
                    consultationDao.updateConsultation(con);
                } else if (user.get().getRole() == Role.TUTOR) {
                    if (con.getStudent() != null){
                        sendEmail(con.getStudent(), "Twoja konsultacja\n " + con.toString() + "\n została odwołana.");
//                        sendSMS(con.getStudent(),  "Twoja konsultacja\n " + con.toString() + "\n została odwołana.");
                    }
                    con.cancel();
                    consultationDao.updateConsultation(con);
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean addConsultationCreatedByStudent(Consultation consultation, String studentUsername, String tutorUsername) {
        Optional<User> student = userDao.findUserByUsername(studentUsername);
        Optional<User> tutor = userDao.findUserByUsername(tutorUsername);
        if (student.isPresent() && tutor.isPresent()) {
            if (student.get().getRole() != Role.STUDENT || tutor.get().getRole() != Role.TUTOR) {
                return false;
            }
            consultation.setStudent(new UserClientInfo(student.get()));
            consultation.setTutor(new UserClientInfo(tutor.get()));
            consultation.setStatus(Status.CREATED_BY_STUDENT);
            if (date_enabled(consultation)){
                consultationDao.insertConsultation(consultation);
                return true;
            }
        }
        return false;
    }

    public List<Consultation> findConsultations(ConsultationSearchForm consultationSearchForm) {
        Query query = new Query(createCriteria(consultationSearchForm));
        return consultationDao.getMongoTemplate().find(query, Consultation.class);
    }

    public long countConsultations(ConsultationSearchForm consultationSearchForm) {
        Query query = new Query(createCriteria(consultationSearchForm));
        return consultationDao.getMongoTemplate().count(query, Consultation.class);
    }

    private Criteria createCriteria(ConsultationSearchForm consultationSearchForm) {
        Criteria criteria = new Criteria();

        if (consultationSearchForm.getDateStart() != null && consultationSearchForm.getDateEnd() != null) {
            criteria = criteria.and("consultationStartTime").gte(consultationSearchForm.getDateStart()).lte(consultationSearchForm.getDateEnd());
        }
        if (consultationSearchForm.getStudentUsername() != null){
            criteria = criteria.and("student.username").is(consultationSearchForm.getStudentUsername());
        }
        if (consultationSearchForm.getTutorUsername() != null){
            criteria = criteria.and("tutor.username").is(consultationSearchForm.getTutorUsername());
        }
        if (consultationSearchForm.getStatus() != null){
            criteria = criteria.and("status").is(consultationSearchForm.getStatus());
        }
        return criteria;
    }


    private void sendEmail(UserClientInfo user, String text) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(user.getUsername());
        simpleMailMessage.setSubject("Zmiana statusu konsultacji");
        simpleMailMessage.setText(text);
        try{
            this.mailSender.send(simpleMailMessage);
        }catch(MailException ex){
            System.out.println(ex.getMessage());
        }
    }

    private void sendSMS(UserClientInfo user, String text){
        Twilio.init("AC4f40f215e9c74f760db0577a5cf922dd", "022c6f1a84e84e033a6f2ca78e5a3f70");
        /*Message message = Message.creator(
                new PhoneNumber(user.getPhoneNumber()),
                new PhoneNumber("+48732484082"),
                text)
                .create();
    */
    }


}
