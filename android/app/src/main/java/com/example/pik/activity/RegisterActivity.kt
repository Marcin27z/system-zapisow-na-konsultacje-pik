package com.example.pik.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.example.pik.R
import com.example.pik.REST.Enum.Role
import com.example.pik.REST.Repository
import com.example.pik.REST.entity.User
import com.example.pik.form.RegistrationForm
import kotlinx.android.synthetic.main.activity_register.*
import java.lang.ref.WeakReference

/**
 * A login screen that offers login via email/password.
 */
class RegisterActivity : AppCompatActivity() {

    /**
     * Keep track of the registration task to ensure we can cancel it if requested.
     */
    private var mRegisterTask: UserRegisterTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        // Set up the login form.
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptRegister()
                return@OnEditorActionListener true
            }
            return@OnEditorActionListener false
        })

        register_button.setOnClickListener { attemptRegister() }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptRegister() {
        if (mRegisterTask != null) {
            return
        }

        // Reset errors.
        email.error = null
        password.error = null
        name.error = null
        surname.error = null

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()
        val nameStr = name.text.toString()
        val surnameStr = surname.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.// Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        // There was an error; don't attempt login and focus the first
        // form field with an error.


        if (TextUtils.isEmpty(surnameStr)) {
            surname.error = getString(R.string.error_field_required)
            focusView = surname
            cancel = true
        }
        if (TextUtils.isEmpty(nameStr)) {
            name.error = getString(R.string.error_field_required)
            focusView = name
            cancel = true
        }
        if (TextUtils.isEmpty(passwordStr)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        }
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            val registrationForm =
                RegistrationForm(emailStr, passwordStr, nameStr, surnameStr)
            mRegisterTask = UserRegisterTask(registrationForm, this)
            mRegisterTask!!.execute(null as Void?)
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            register_form.visibility = if (show) View.GONE else View.VISIBLE
            register_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        register_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

            register_progress.visibility = if (show) View.VISIBLE else View.GONE
            register_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        register_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            register_progress.visibility = if (show) View.VISIBLE else View.GONE
            register_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    fun registrationSuccessful() {
        Toast.makeText(this, "Registration successful!!!", Toast.LENGTH_LONG).show()
    }

    fun registrationUnsuccessful() {
        Toast.makeText(this, "Something went wrong!!!", Toast.LENGTH_LONG).show()
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserRegisterTask internal constructor(registrationForm: RegistrationForm, context: Context) :
        AsyncTask<Void, Void, Boolean>() {

        private val context: WeakReference<Context> = WeakReference(context)
        private val registrationForm: WeakReference<RegistrationForm> = WeakReference(registrationForm)

        override fun doInBackground(vararg params: Void): Boolean? {
            val repository = Repository(context.get()!!)
            val user = User()
            val registrationForm = registrationForm.get()!!
            user.username = registrationForm.email
            user.password = registrationForm.password
            user.role = Role.STUDENT
            user.name = registrationForm.name
            user.surname = registrationForm.surname
            return repository.register(user)
        }

        override fun onPostExecute(success: Boolean?) {
            mRegisterTask = null
            showProgress(false)

            if (success!!) {
                finish()
                registrationSuccessful()
            } else {
                registrationUnsuccessful()
            }
        }

        override fun onCancelled() {
            mRegisterTask = null
            showProgress(false)
        }
    }
}
