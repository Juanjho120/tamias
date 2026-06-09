import { CurrencyPipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'quetzalCurrency',
  standalone: true
})
export class QuetzalCurrencyPipe implements PipeTransform {
  private readonly currencyPipe = new CurrencyPipe('es-GT');

  transform(value: number | string | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }

    const numericValue = typeof value === 'number' ? value : Number(value);

    if (Number.isNaN(numericValue)) {
      return '—';
    }

    return this.currencyPipe.transform(
      numericValue,
      'GTQ',
      'symbol-narrow',
      '1.2-2',
      'es-GT'
    ) ?? '—';
  }
}
