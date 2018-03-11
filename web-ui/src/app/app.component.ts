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
      'message.serverUnavailable': 'The server unavailable, please try again in a minute',
      'action.find': 'Find',
      'label.transactionId': 'Transaction Id'
    };
  }
}
