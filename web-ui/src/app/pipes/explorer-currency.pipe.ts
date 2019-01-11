import { Pipe, PipeTransform } from '@angular/core';
import { Config } from '../config';

@Pipe({
  name: 'explorerCurrency'
})
export class ExplorerCurrencyPipe implements PipeTransform {
  transform(currency: any): string {
    return `${ currency } ${ Config.currentCurrency }`;
  }
}
