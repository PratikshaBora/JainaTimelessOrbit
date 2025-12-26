import { TestBed } from '@angular/core/testing';
import { WebsocketService } from './websocket';

describe('WebsocketService', () => {
  let service: WebsocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WebsocketService] // register the service
    });
    service = TestBed.inject(WebsocketService); // inject the correct class
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
