import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common'

@Pipe({
  name: 'explorerDatetime'
})
export class ExplorerDatetimePipe extends DatePipe implements PipeTransform {
  private calendarDateFormat = 'MMMM d, y, h:mm:ss a';

  transform(datetime: any): string {
    return super.transform(datetime, this.calendarDateFormat);
  }
}
