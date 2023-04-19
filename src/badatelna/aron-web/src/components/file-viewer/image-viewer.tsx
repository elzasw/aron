import classNames from 'classnames';
import OpenSeadragon from 'openseadragon';
import React, { useEffect, useImperativeHandle, useRef, useState } from 'react';
import { API_URL } from '../../enums';
import { useLayoutStyles } from '../../styles';
import { useStyles } from './styles';

export interface ImageViewerExposedFunctions {
  zoomIn: () => void;
  zoomOut: () => void;
  rotateRight: () => void;
  rotateLeft: () => void;
  reset: () => void;
}

interface ImageViewerProps {
  id: number | string;
  url?: string;
}

export const ImageViewer = React.forwardRef<ImageViewerExposedFunctions, ImageViewerProps>(({
  id,
  url,
}, ref) => {
  const [viewer, setViewer] = useState<OpenSeadragon.Viewer>();
  const viewerRef = useRef<HTMLDivElement>(null);
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  useImperativeHandle(
    ref,
    () => {
      return {
        zoomIn: () => {
          if (viewer) {
            const nextZoom = viewer.viewport.getZoom() * 1.5;
            const maxZoom = viewer.viewport.getMaxZoom();

            viewer.viewport.zoomTo(nextZoom > maxZoom ? maxZoom : nextZoom);
          }
        },
        zoomOut: () => {
          if (viewer) {
            const nextZoom = viewer.viewport.getZoom() / 1.5;
            const minZoom = viewer.viewport.getMinZoom();

            viewer.viewport.zoomTo(nextZoom < minZoom ? minZoom : nextZoom);
          }
        },
        rotateLeft: () => {
          if (viewer) {
            const currentRotation = viewer.viewport.getRotation()
            viewer.viewport.setRotation(currentRotation - 90)
          }
        },
        rotateRight: () => {
          if (viewer) {
            const currentRotation = viewer.viewport.getRotation()
            viewer.viewport.setRotation(currentRotation + 90)
          }
        },
        reset: () => viewer && viewer.viewport.goHome(),
      }
    }, [viewer])

  useEffect(() => {
    const tileSources = url ? url : `${API_URL}/tile/${id}/image.dzi`;
    if (viewerRef.current && id) {
      // viewer && viewer.destroy();

      if (!viewer) {
        setViewer(
          OpenSeadragon({
            element: viewerRef.current,
            showSequenceControl: false,
            showNavigationControl: false,
            showZoomControl: false,
            showHomeControl: false,
            showFullPageControl: false,
            tileSources,
            maxZoomPixelRatio: 5,
          })
        );
      }
      else {
        viewer.open(tileSources);
      }
    }
  }, [id, setViewer, viewerRef.current]);

  return <div className={classNames(classes.fileViewer, layoutClasses.flexCentered)}>
    <div {...{ ref: viewerRef, className: classes.imageViewer }} />;
    </div>
})
