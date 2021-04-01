import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'explorerAmount'
})
export class ExplorerAmountPipe implements PipeTransform {
  zeroFill( number: string, width: number ): string {
    let digits = number.split(".");
    if (digits.length > 1) {
      width -= digits.pop().length;
    } else {
      number += ".";
    }

    if ( width > 0 )
    {
      return number + new Array( width + 1 ).join( '0' );
    }
    return number + ""; // always return a string
  }

  transform(currency: any, digit: number = 8, fillZero:Boolean = false): string {
    let currencyNumber: number;
    if (typeof(currency) === 'number') {
      currencyNumber = currency;
    } else if (typeof(currency) === 'string') {
      currencyNumber = parseFloat(currency);
    } else {
      return '0';
    }
    
    return fillZero ? `${ this.zeroFill(currencyNumber.toLocaleString(undefined, { maximumFractionDigits: digit }), digit) }` : `${ currencyNumber.toLocaleString(undefined, { maximumFractionDigits: digit }) }`;
  }

}
