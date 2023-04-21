import classNames from 'classnames';
import OpenSeadragon from 'openseadragon';
import React, { useEffect, useImperativeHandle, useRef, useState } from 'react';
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
  parentId: number | string;
  urls?: string[];
  page?: number;
}

export const ImageViewer = React.forwardRef<ImageViewerExposedFunctions, ImageViewerProps>(({
  parentId,
  urls,
  page = 0,
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
    if (viewerRef.current) {

      if (!viewer) {
        const newViewer = OpenSeadragon({
          tileSources: urls,
          initialPage: page,
          maxZoomPixelRatio: 5,
          sequenceMode: true,
          element: viewerRef.current,
          showNavigationControl: false,
          showZoomControl: false,
          showHomeControl: false,
          showFullPageControl: false,
          showSequenceControl: false,
        })
        newViewer.addHandler("open", () => {
          newViewer.setVisible(true);
        })
        newViewer.addHandler("open-failed", () => {
          newViewer.setVisible(false);
        })
        setViewer(newViewer);
      }
    }
  }, [setViewer, viewerRef.current]);

  useEffect(() => {
    if (viewer) {
      viewer?.open(urls || [], page);
    }
  }, [parentId])

  useEffect(() => {
    if (viewer && page !== viewer.currentPage()) {
      viewer?.goToPage(page)
    }
  }, [page])

  return <div className={classNames(classes.fileViewer, layoutClasses.flexCentered)}>
    <div {...{ ref: viewerRef, className: classes.imageViewer }} />;
    </div>
})
