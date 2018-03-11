import { Component } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';

import { DEFAULT_LANG, LanguageService } from './services/language.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  constructor(
    private translate: TranslateService,
    private languageService: LanguageService) {

    translate.setDefaultLang(DEFAULT_LANG);
    translate.use(languageService.getLang());

    // define langs
    translate.setTranslation('en', this.englishLang());
  }

  englishLang(): Object {
    return {
      // default messages from angular
      'required': 'A value is required',
      'pattern': 'Invalid format',
      'email': 'Invalid email',
      'minlength': 'More characters are required',
      'maxlength': 'Too many characters',
      'min': 'The value is too small',
      'max': 'The value is too big',

      // messages
      'message.serverUnavailable': 'The server unavailable, please try again in a minute',

      // actions
      'action.find': 'Find',

      // labels
      'label.transactionId': 'Transaction Id',
      'label.confirmations': 'Confirmations',
      'label.blockhash': 'Block Hash',
      'label.blocktime': 'Block Time',
      'label.inputAddresses': 'Senders',
      'label.outputAddresses': 'Receivers'
    };
  }
}
