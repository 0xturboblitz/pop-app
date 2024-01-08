import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

// Import the native module. On web, it will be resolved to Ocr_module.web.ts
// and on native platforms to Ocr_module.ts
import Ocr_moduleModule from './src/Ocr_moduleModule';
import Ocr_moduleView from './src/Ocr_moduleView';
import { ChangeEventPayload, Ocr_moduleViewProps } from './src/Ocr_module.types';

// Get the native constant value.
export const PI = Ocr_moduleModule.PI;

export function hello(): string {
  return Ocr_moduleModule.hello();
}

export async function setValueAsync(value: string) {
  return await Ocr_moduleModule.setValueAsync(value);
}

const emitter = new EventEmitter(Ocr_moduleModule ?? NativeModulesProxy.Ocr_module);

export function addChangeListener(listener: (event: ChangeEventPayload) => void): Subscription {
  return emitter.addListener<ChangeEventPayload>('onChange', listener);
}

export { Ocr_moduleView, Ocr_moduleViewProps, ChangeEventPayload };
