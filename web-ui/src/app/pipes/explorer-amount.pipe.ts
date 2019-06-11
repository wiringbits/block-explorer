import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'explorerAmount'
})
export class ExplorerAmountPipe implements PipeTransform {

  transform(currency: any): string {
    let currencyNumber: number;
    if (typeof(currency) === 'number') {
      currencyNumber = currency;
    } else if (typeof(currency) === 'string') {
      currencyNumber = parseFloat(currency);
    }

    return `${ currencyNumber.toFixed(8) } `;
  }

}
