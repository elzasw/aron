import { API_URL } from '../enums';

export const downloadFileFromUrl = (url: string, name = '', blank = false) => {
  if (blank) {
    window.open(url, '_blank');
  } else {
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
};

export const downloadFileByUrl = (url: string, name = '', blank = false) =>
  downloadFileFromUrl(`${window.location.origin}${API_URL}${url}`, name, blank);

export const blobToBase64 = (blob: Blob, callback: (s: string) => any) => {
  const reader = new FileReader();
  reader.onload = function () {
    const dataUrl = reader.result as string;
    const base64 = dataUrl.split(',')[1];
    callback(base64);
  };
  reader.readAsDataURL(blob);
};
