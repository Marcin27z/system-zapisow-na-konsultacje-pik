import { Component, OnInit } from '@angular/core';
import {Principal} from '../../rejestration/resource/principal';
import {ActivatedRoute, Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-main-strip-tutor',
  templateUrl: './main-strip-tutor.component.html',
  styleUrls: ['./main-strip-tutor.component.css']
})
export class MainStripTutorComponent implements OnInit {

  user: Principal;
  constructor(private route: ActivatedRoute, private http: HttpClient, private router: Router) {
  }

  ngOnInit() {
    this.user = new Principal();
    this.user.username = this.route.snapshot.params['username'];
    this.user.password = this.route.snapshot.params['password'];
  }

}
