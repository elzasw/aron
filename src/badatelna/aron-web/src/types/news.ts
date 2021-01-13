interface Attachment {
  link: string;
  name: string;
}

export interface NewsEntity {
  date: string;
  name: string;
  text: string;
  attachments?: Attachment[];
}
