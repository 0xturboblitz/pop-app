import * as React from 'react';

import { Ocr_moduleViewProps } from './Ocr_module.types';

export default function Ocr_moduleView(props: Ocr_moduleViewProps) {
  return (
    <div>
      <span>{props.name}</span>
    </div>
  );
}
