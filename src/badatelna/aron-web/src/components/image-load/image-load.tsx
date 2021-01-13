import React, { useState, useEffect, ReactElement, ReactNode } from 'react';
import { blobToBase64, getFile } from '../../common-utils';

interface ImageLoadProps {
  id?: string;
  alternativeImage: ReactNode;
  className: string;
}

export function ImageLoad({
  id,
  alternativeImage,
  className,
}: ImageLoadProps): ReactElement {
  const [imgBase64, setImgBase64] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      const blob = await getFile(id);
      blob
        ? blobToBase64(blob, (base) => {
            setImgBase64(base);
          })
        : setImgBase64(null);
    })();
  }, [id]);

  return (
    <div {...{ className }}>
      {imgBase64 ? <img src={imgBase64} /> : alternativeImage}
    </div>
  );
}
