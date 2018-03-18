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
      'message.unknownErrors': 'Unknown error, please try again in a minute',
      'message.transactionNotFound': 'Transaction not found',
      'message.addressNotFound': 'Address not found',
      'message.blockNotFound': 'Block not found',

      // actions
      'action.find': 'Find',

      // labels
      'label.coinName': 'XSN',
      'label.transactionId': 'Transaction Id',
      'label.confirmations': 'Confirmations',
      'label.blockhash': 'Block Hash',
      'label.blocktime': 'Block Time',
      'label.medianTime': 'Median Time',
      'label.noInput': 'No input',
      'label.coinbase': 'Coinbase',
      'label.output': 'Receivers',
      'label.from': 'From',
      'label.to': 'To',
      'label.value': 'Amount',
      'label.fee': 'Fee',

      'label.address': 'Address',
      'label.balance': 'Balance',
      'label.received': 'Received',
      'label.spent': 'Spent',
      'label.transactionCount': 'Transactions',

      'label.blockType': 'Block type',
      'label.next': 'Next',
      'label.previous': 'Previous',
      'label.merkleRoot': 'Merkle root',
      'label.size': 'Size',
      'label.version': 'Version',
      'label.nonce': 'Nonce',
      'label.bits': 'Bits',
      'label.chainwork': 'Chainwork',
      'label.difficulty': 'Difficulty',
      'label.transactions': 'Transactions',
      'label.rewards': 'Rewards',
      'label.coinstake': 'Coinstake',
      'label.masternode': 'Master node',
      'label.amount': 'Amount',
      'label.blockReward': 'Block reward'
    };
  }
}
