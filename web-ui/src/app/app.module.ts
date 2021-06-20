import { NgModule, CUSTOM_ELEMENTS_SCHEMA  } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AlertModule } from 'ngx-bootstrap/alert';
import { TabsModule } from 'ngx-bootstrap/tabs';
import { TranslateModule } from '@ngx-translate/core';
import { ToastrModule } from 'ngx-toastr';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgHttpLoaderModule } from 'ng-http-loader';
import { LottieAnimationViewModule } from 'ng-lottie';
import { SliderModule } from '@syncfusion/ej2-angular-inputs';
import { UiSwitchModule } from 'ngx-ui-switch';
import { TooltipModule } from 'ng2-tooltip-directive';
import { PopoverModule } from 'ngx-bootstrap/popover';

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
import { OrderBookService } from './services/orderbook.service';

import { AppComponent } from './app.component';
import { TrezorConnectComponent } from './components/trezor-connect/trezor-connect.component';
import { SharedModule } from './components/shared/shared.module';
import { PipesModule } from './pipes/pipes.module';
import { TposnodesService } from './services/tposnodes.service';
import { CalculatorComponent } from './components/calculator/calculator.component';
import { DexMonitorComponent } from './components/dex-monitor/dex-monitor.component';

import { FocusDirective } from './directives/focus-directive';

@NgModule({
  declarations: [
    AppComponent,
    TrezorConnectComponent,
    CalculatorComponent,
    DexMonitorComponent,
    FocusDirective
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
    ReactiveFormsModule,
    LottieAnimationViewModule.forRoot(),
    SliderModule,
    UiSwitchModule,
    TooltipModule,
    PopoverModule.forRoot()
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
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
    XSNService,
    OrderBookService
  ],
  bootstrap: [AppComponent],
  
})
export class AppModule { }
