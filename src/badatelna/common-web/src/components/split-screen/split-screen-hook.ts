import { useState, useRef, useEffect, MouseEvent } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { unstable_batchedUpdates } from 'react-dom';

interface SplitScreenHookProps {
  midPoint: number;
  leftMinWidth: number;
  rightMinWidth: number;
}

export function useSplitScreen({
  leftMinWidth,
  midPoint,
  rightMinWidth,
}: SplitScreenHookProps) {
  const [leftSize, setLeftSize] = useState(midPoint);

  /**
   * Dragging logic
   * The DragEnd/Drag handles are attached to document
   * The DragStart handle is attached to dragger element
   * after mousedown on the dragger, the drag is activated (dragActive.current -> true)
   * after mouseUp on the document, the drag is deactivated (dragActive.current -> false)
   */
  const dragActive = useRef(false);

  const handleDragStart = useEventCallback((e: MouseEvent) => {
    dragActive.current = true;
    e.preventDefault();
  });

  const handleDragEnd = useEventCallback(() => {
    dragActive.current = false;
  });

  const handleDrag = useEventCallback((e: globalThis.MouseEvent) => {
    if (!dragActive.current) {
      return;
    }
    if (e.clientX < leftMinWidth) {
      setLeftSize(leftMinWidth);
    } else if (e.clientX > window.innerWidth - rightMinWidth) {
      setLeftSize(window.innerWidth - rightMinWidth);
    } else {
      setLeftSize(e.clientX);
    }
  });

  useEffect(() => {
    document.addEventListener('mousemove', handleDrag);
    document.addEventListener('mouseup', handleDragEnd);
    return () => {
      document.removeEventListener('mousemove', handleDrag);
      document.removeEventListener('mouseup', handleDragEnd);
    };
  }, []);

  /**
   * Logic to controll the drag position through actions
   * The transitionOn property is necessary to provide smooth width transition after button click
   */
  const [transitionOn, setTransitionOn] = useState(false);

  const handleFullSizeLeft = useEventCallback(() => {
    unstable_batchedUpdates(() => {
      setLeftSize(window.innerWidth - rightMinWidth);
      setTransitionOn(true);
    });
    setTimeout(() => {
      setTransitionOn(false);
    }, 500);
  });

  const handleFullSizeRight = useEventCallback(() => {
    unstable_batchedUpdates(() => {
      setLeftSize(leftMinWidth);
      setTransitionOn(true);
    });
    setTimeout(() => {
      setTransitionOn(false);
    }, 500);
  });

  const handleMoveToMiddle = useEventCallback(() => {
    unstable_batchedUpdates(() => {
      setLeftSize(midPoint);
      setTransitionOn(true);
    });
    setTimeout(() => {
      setTransitionOn(false);
    }, 500);
  });

  const handleMoveRight = useEventCallback(() => {
    if (leftSize < midPoint && midPoint - leftSize > 80) {
      handleMoveToMiddle();
    } else {
      handleFullSizeLeft();
    }
  });

  const handleMoveLeft = useEventCallback(() => {
    if (leftSize > midPoint && leftSize - midPoint > 80) {
      handleMoveToMiddle();
    } else {
      handleFullSizeRight();
    }
  });

  const isLeftOnFullscreen = useEventCallback(() => {
    return leftSize === window.innerWidth - rightMinWidth;
  });

  const isRightOnFullscreen = useEventCallback(() => {
    return leftSize === 0;
  });

  return {
    leftSize,
    transitionOn,
    handleDragStart,
    handleFullSizeLeft,
    handleMoveToMiddle,
    handleFullSizeRight,
    handleMoveLeft,
    handleMoveRight,
    isLeftOnFullscreen,
    isRightOnFullscreen,
  };
}
