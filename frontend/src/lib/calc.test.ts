import { describe, expect, it } from 'vitest'
import { amountInWords, calcLine, calcTotals, round2 } from './calc'

describe('calc (mirrors the workbook formulas)', () => {
  it('Sheet1 example: 30 kg x 40 @ 9% + 9% = 1416', () => {
    const l = calcLine({ qty: 30, rate: 40, cgstRate: 9, sgstRate: 9 })
    expect(l).toEqual({ taxable: 1200, cgst: 108, sgst: 108, total: 1416 })
    const t = calcTotals([l])
    expect(t).toEqual({ taxable: 1200, cgst: 108, sgst: 108, tax: 216, grand: 1416 })
  })
  it('handles 2.5% rates', () => {
    expect(calcLine({ qty: 2, rate: 13, cgstRate: 2.5, sgstRate: 2.5 })).toEqual({ taxable: 26, cgst: 0.65, sgst: 0.65, total: 27.3 })
  })
  it('rounds half up to 2 decimals', () => {
    expect(round2(1.005)).toBe(1.01)
    const l = calcLine({ qty: 3, rate: 33.33, cgstRate: 9, sgstRate: 9 })
    expect(l.taxable).toBe(99.99)
    expect(l.cgst).toBe(9)
  })
  it('amount in words (Indian system)', () => {
    expect(amountInWords(1416, 'INR')).toBe('INR One Thousand Four Hundred Sixteen Only')
    expect(amountInWords(1416.5, 'INR')).toBe('INR One Thousand Four Hundred Sixteen and Fifty Paise Only')
    expect(amountInWords(1234567, 'INR')).toBe('INR Twelve Lakh Thirty Four Thousand Five Hundred Sixty Seven Only')
    expect(amountInWords(0, 'INR')).toBe('INR Zero Only')
  })
})
