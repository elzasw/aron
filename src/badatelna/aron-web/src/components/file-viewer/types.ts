import { FileType } from './enums';

// TODO:
export type ViewerProps = any;

// export interface ViewerProps {
//   file: any;
//   scale: number;
//   pageNumber: number;
// }

export interface WrapperProps {
  id?: string;
  fileType?: FileType;
  highResImage?: boolean;
  children: (props: any) => JSX.Element;
}
