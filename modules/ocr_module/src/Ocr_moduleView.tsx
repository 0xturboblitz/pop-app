import { requireNativeViewManager } from 'expo-modules-core';
import * as React from 'react';

import { Ocr_moduleViewProps } from './Ocr_module.types';

const NativeView: React.ComponentType<Ocr_moduleViewProps> =
  requireNativeViewManager('Ocr_module');

export default function Ocr_moduleView(props: Ocr_moduleViewProps) {
  return <NativeView {...props} />;
}
