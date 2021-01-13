import React, { useState, useEffect, useCallback } from 'react';
import OpenSeadragon from 'openseadragon';

import { WrapperProps } from './types';
import { API_URL } from '../../enums';
import { useRef } from '../../common-utils';

function Component({ id, children: Children, viewer, setViewer }: any) {
  const [current, viewerRef] = useRef();

  const getZoom = useCallback(
    () => viewer.viewport.viewportToImageZoom(viewer.viewport.getZoom()),
    [viewer]
  );

  const zoomIn = useCallback(() => {
    if (getZoom() < 1.1) {
      viewer.viewport.zoomBy(1 / 0.7);
    } else {
      viewer.viewport.zoomTo(viewer.viewport.imageToViewportZoom(1.1));
    }
  }, [viewer, getZoom]);

  const zoomOut = useCallback(() => {
    if (getZoom() > 0.2) {
      viewer.viewport.zoomBy(0.7);
    } else {
      viewer.viewport.zoomTo(viewer.viewport.imageToViewportZoom(0.2));
    }
  }, [viewer, getZoom]);

  const reset = useCallback(() => viewer.viewport.goHome(), [viewer]);

  useEffect(() => {
    if (current && id) {
      setViewer(
        OpenSeadragon({
          element: current,
          showSequenceControl: false,
          showNavigationControl: false,
          showZoomControl: false,
          showHomeControl: false,
          showFullPageControl: false,
          tileSources: `${API_URL}/tile/${id}/image.dzi`,
        })
      );
    }
  }, [current, id, setViewer]);

  return (
    <Children
      {...{
        zoomIn,
        zoomOut,
        reset,
        fileViewerProps: {
          viewerRef,
        },
      }}
    />
  );
}

export function ImageViewerWrapper(props: WrapperProps) {
  const [viewer, setViewer] = useState();

  return <Component {...{ ...props, viewer, setViewer }} />;
}
