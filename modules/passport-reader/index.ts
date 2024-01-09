import { NativeModulesProxy, EventEmitter } from 'expo-modules-core';

// Import the native module. On web, it will be resolved to PassportReader.web.ts
// and on native platforms to PassportReader.ts
import PassportReaderModule from './src/PassportReaderModule';

// Get the native constant value.
export const PI = PassportReaderModule.PI;

export function hello(): string {
  return PassportReaderModule.hello();
}

export async function setValueAsync(value: string) {
  return await PassportReaderModule.setValueAsync(value);
}

export async function scan({ documentNumber="19HA34828", dateOfBirth="000719", dateOfExpiry="291209", quality=1 }) {
  assert(typeof documentNumber === 'string', 'expected string "documentNumber"')
  assert(isDate(dateOfBirth), 'expected string "dateOfBirth" in format "yyMMdd"')
  assert(isDate(dateOfExpiry), 'expected string "dateOfExpiry" in format "yyMMdd"')
  return PassportReaderModule.scan({ documentNumber, dateOfBirth, dateOfExpiry, quality })
}

function assert (statement: any, err: any) {
  if (!statement) {
    throw new Error(err || 'Assertion failed')
  }
}

function isDate (str: string) {
  const DATE_REGEX = /^\d{6}$/
  return typeof str === 'string' && DATE_REGEX.test(str)
}

const emitter = new EventEmitter(PassportReaderModule ?? NativeModulesProxy.PassportReader);

// export function addChangeListener(listener: (event: ChangeEventPayload) => void): Subscription {
//   return emitter.addListener<ChangeEventPayload>('onChange', listener);
// }