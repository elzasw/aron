import { abortableFetch } from 'utils/abortable-fetch';

export function callUploadFile(fileUrl: string, file: File) {
  const formData = new FormData();
  formData.append('file', file, file.name);

  return abortableFetch(`${fileUrl}`, {
    method: 'POST',
    body: formData,
  });
}
