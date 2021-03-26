import { Pipe, PipeTransform } from '@angular/core';
import { Config } from '../config';

@Pipe({
  name: 'explorerCurrency'
})
export class ExplorerCurrencyPipe implements PipeTransform {
  transform(currency: any): string {
    let currencyNumber: number;
    if (typeof(currency) === 'number') {
      currencyNumber = currency;
    } else if (typeof(currency) === 'string') {
      currencyNumber = parseFloat(currency);
    } else {
      return `0 ${ Config.currentCurrency }`;
    }

    return `${ currencyNumber.toLocaleString(undefined, { maximumFractionDigits: 8 }) } ${ Config.currentCurrency }`;
  }
}
