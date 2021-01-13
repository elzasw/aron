// TODO:
export type ViewerProps = any;

// export interface ViewerProps {
//   file: any;
//   scale: number;
//   pageNumber: number;
// }

export interface WrapperProps {
  id: string;
  children: (props: any) => JSX.Element;
}
