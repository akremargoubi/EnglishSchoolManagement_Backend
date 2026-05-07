import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class CorsInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Clone the request and add headers to avoid preflight
    const modifiedReq = req.clone({
      setHeaders: {
        'Content-Type': 'application/json'
      }
    });
    return next.handle(modifiedReq);
  }
}