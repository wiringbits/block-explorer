import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'explorerCurrency'
})
export class ExplorerCurrencyPipe implements PipeTransform {
  transform(currency: any): string {
    return `${ currency } XSN`;
  }
}
