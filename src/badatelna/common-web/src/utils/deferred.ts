/**
 * Promise holder.
 */
export class Deferred<T> {
  private _promise: Promise<T>;
  private _resolve: (value?: T | PromiseLike<T>) => void = () => {};
  private _reject: (reason?: any) => void = () => {};

  constructor() {
    this._promise = new Promise<T>((resolve, reject) => {
      this._resolve = resolve;
      this._reject = reject;
    });
  }

  /**
   * Stored promise.
   */
  get promise(): Promise<T> {
    return this._promise;
  }

  /**
   * Resolves the stored promise.
   */
  public resolve = (value?: T | PromiseLike<T>): void => {
    this._resolve(value);
  };

  /**
   * Rejects the stored promise.
   */
  public reject = (reason?: any): void => {
    this._reject(reason);
  };
}
