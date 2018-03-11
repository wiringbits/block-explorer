import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AlertModule, BsDropdownModule, CollapseModule, TooltipModule, ModalModule } from 'ngx-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { ToastrModule } from 'ngx-toastr';

import { NgHttpLoaderModule } from 'ng-http-loader/ng-http-loader.module'

import { LanguageService } from './services/language.service';
import { NotificationService } from './services/notification.service';
import { TransactionsService } from './services/transactions.service';

import { AppComponent } from './app.component';
import { HomeComponent } from './components/home/home.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { TransactionDetailsComponent } from './components/transaction-details/transaction-details.component';
import { TransactionFinderComponent } from './components/transaction-finder/transaction-finder.component';

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    FooterComponent,
    NavbarComponent,
    TransactionDetailsComponent,
    TransactionFinderComponent
  ],
  imports: [
    AppRoutingModule,
    AlertModule.forRoot(),
    BsDropdownModule.forRoot(),
    CollapseModule.forRoot(),
    TooltipModule.forRoot(),
    ModalModule.forRoot(),
    CommonModule,
    BrowserAnimationsModule,
    ToastrModule.forRoot(),
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    NgHttpLoaderModule,
    TranslateModule.forRoot(),
  ],
  providers: [
    LanguageService,
    NotificationService,
    TransactionsService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
