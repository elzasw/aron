export const getCookieValue = (cookieName: string) => {
  let decodedCookie = decodeURIComponent(document.cookie);
  let cookieArray = decodedCookie.split(';');
  const cookie = cookieArray.find((cookie) => cookie.trim().startsWith(`${cookieName}=`))

  return cookie?.substring(cookie.indexOf("=") + 1);
}
