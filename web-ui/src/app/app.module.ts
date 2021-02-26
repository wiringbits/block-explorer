import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AlertModule } from 'ngx-bootstrap';
import { TabsModule } from 'ngx-bootstrap/tabs';
import { TranslateModule } from '@ngx-translate/core';
import { ToastrModule } from 'ngx-toastr';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { NgHttpLoaderModule } from 'ng-http-loader';

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
import { TrezorRepositoryService } from './services/trezor-repository.service';
import { TposContractsService } from './services/tposcontracts.service';
import { XSNService } from './services/xsn.service';

import { AppComponent } from './app.component';
import { TrezorConnectComponent } from './components/trezor-connect/trezor-connect.component';
import { SharedModule } from './components/shared/shared.module';
import { PipesModule } from './pipes/pipes.module';
import { TposnodesService } from './services/tposnodes.service';
import { CalculatorComponent } from './components/calculator/calculator.component';

@NgModule({
  declarations: [
    AppComponent,
    TrezorConnectComponent,
    CalculatorComponent
  ],
  imports: [
    AppRoutingModule,
    FormsModule,
    SharedModule,
    PipesModule,
    BrowserAnimationsModule,
    ToastrModule.forRoot(),
    HttpClientModule,
    NgHttpLoaderModule,
    TranslateModule.forRoot(),
    TabsModule.forRoot(),
    AlertModule.forRoot(),
    ReactiveFormsModule
  ],
  providers: [
    AddressesService,
    BalancesService,
    BlocksService,
    ErrorService,
    LanguageService,
    MasternodesService,
    TposnodesService,
    NavigatorService,
    NotificationService,
    TickerService,
    TransactionsService,
    TrezorRepositoryService,
    TposContractsService,
    XSNService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
