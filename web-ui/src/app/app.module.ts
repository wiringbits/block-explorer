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

import { NgHttpLoaderModule } from 'ng-http-loader/ng-http-loader.module';
import { NgxPaginationModule } from 'ngx-pagination';

import { AddressesService } from './services/addresses.service';
import { BalancesService } from './services/balances.service';
import { BlocksService } from './services/blocks.service';
import { ErrorService } from './services/error.service';
import { LanguageService } from './services/language.service';
import { MasternodesService } from './services/masternodes.service';
import { NavigatorService } from './services/navigator.service';
import { NotificationService } from './services/notification.service';
import { TickerService } from './services/ticker.service';
import { TransactionsService } from './services/transactions.service';

import { AppComponent } from './app.component';
import { HomeComponent } from './components/home/home.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { TransactionDetailsComponent } from './components/transaction-details/transaction-details.component';
import { FinderComponent } from './components/finder/finder.component';
import { AddressDetailsComponent } from './components/address-details/address-details.component';
import { BlockDetailsComponent } from './components/block-details/block-details.component';
import { LatestBlocksComponent } from './components/latest-blocks/latest-blocks.component';
import { TickerComponent } from './components/ticker/ticker.component';
import { RichestAddressesComponent } from './components/richest-addresses/richest-addresses.component';
import { MasternodesComponent } from './components/masternodes/masternodes.component';

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    FooterComponent,
    NavbarComponent,
    TransactionDetailsComponent,
    FinderComponent,
    AddressDetailsComponent,
    BlockDetailsComponent,
    LatestBlocksComponent,
    TickerComponent,
    RichestAddressesComponent,
    MasternodesComponent
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
    NgxPaginationModule
  ],
  providers: [
    AddressesService,
    BalancesService,
    BlocksService,
    ErrorService,
    LanguageService,
    MasternodesService,
    NavigatorService,
    NotificationService,
    TickerService,
    TransactionsService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
