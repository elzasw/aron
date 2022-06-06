import React, { useState, useEffect, useCallback } from 'react';
import OpenSeadragon from 'openseadragon';

import { WrapperProps } from './types';
import { API_URL } from '../../enums';
import { useRef } from '../../common-utils';

function Component({
  id,
  fileType,
  children: Children,
  viewer,
  setViewer,
}: WrapperProps & {
  viewer?: OpenSeadragon.Viewer;
  setViewer: (viewer: OpenSeadragon.Viewer) => void;
}) {
  const [current, viewerRef] = useRef();

  const getZoom = useCallback(
    () =>
      viewer
        ? viewer.viewport.viewportToImageZoom(viewer.viewport.getZoom())
        : 0,
    [viewer]
  );

  const zoomIn = useCallback(() => {
    if (viewer) {
      if (getZoom() < 1.1) {
        viewer.viewport.zoomBy(1 / 0.7);
      } else if (viewer) {
        viewer.viewport.zoomTo(viewer.viewport.imageToViewportZoom(1.1));
      }
    }
  }, [viewer, getZoom]);

  const zoomOut = useCallback(() => {
    if (viewer) {
      if (getZoom() > 0.2) {
        viewer.viewport.zoomBy(0.7);
      } else if (viewer) {
        viewer.viewport.zoomTo(viewer.viewport.imageToViewportZoom(0.2));
      }
    }
  }, [viewer, getZoom]);

  const rotateRight = useCallback(() => {
    if(viewer){
      const currentRotation = viewer.viewport.getRotation()
      viewer.viewport.setRotation(currentRotation + 90)
    }
  }, [viewer])

  const rotateLeft = useCallback(() => {
    if(viewer){
      const currentRotation = viewer.viewport.getRotation()
      viewer.viewport.setRotation(currentRotation - 90)
    }
  }, [viewer])

  const reset = useCallback(() => viewer && viewer.viewport.goHome(), [viewer]);

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
          maxZoomPixelRatio: 5,
        })
      );
    }
  }, [current, id, setViewer]);

  // disable context menu to prevent image saving
  useEffect(() => {
    viewer?.element?.addEventListener("contextmenu", (event) => { 
      console.log("test");
      event.preventDefault();
    })  
  },[viewer])

  return (
    <Children
      {...{
        zoomIn,
        zoomOut,
        rotateLeft,
        rotateRight,
        reset,
        fileViewerProps: { fileType, viewerRef },
      }}
    />
  );
}

export function ImageViewerWrapper(props: WrapperProps) {
  const [viewer, setViewer] = useState<OpenSeadragon.Viewer>();

  return <Component {...{ ...props, viewer, setViewer }} />;
}
