import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from './login/login.component';

const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'ingreso', component: LoginComponent }
];

@NgModule({
  declarations: [LoginComponent],
  // FormsModule ademas del reactivo: el checkbox "Recordar mi usuario" usa
  // ngModel standalone porque no pertenece al contrato del formulario.
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SharedModule, RouterModule.forChild(routes)]
})
export class AuthModule { }
