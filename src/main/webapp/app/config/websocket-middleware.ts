import SockJS from 'sockjs-client';

import Stomp, { Client, Subscription } from 'webstomp-client';
import { Observable, Subscriber } from 'rxjs';
import { Storage } from 'react-jhipster';

import { Middleware } from 'redux';

import { getAccount, logoutSession } from 'app/shared/reducers/authentication';
import { updateHeater } from 'app/modules/heater/heater.reducer';
import { IRootState } from 'app/config/store';

import type {} from 'redux-thunk/extend-redux';

let stompClient: Client | null = null;

let subscriber: Subscription | null | undefined = null;
let connection: Promise<any>;
let connectedPromise: any = null;

let alreadyConnectedOnce = false;

const createConnection = (): Promise<any> => new Promise(resolve => (connectedPromise = resolve));

export const sendActivity = (page: string) => {
  connection?.then(() => {
    stompClient?.send(
      '/topic/activity', // destination
      JSON.stringify({ page }), // body
      {} // header
    );
  });
};

const subscribe = async (topic: string): Promise<Observable<any>> => {
  await connection;
  let listenerObserver: Subscriber<any> | undefined;
  const listener = new Observable(observer => {
    listenerObserver = observer;
  });
  subscriber = stompClient?.subscribe(topic, data => {
    listenerObserver?.next(JSON.parse(data.body));
  });
  return listener;
};

const connect = () => {
  if (connectedPromise !== null || alreadyConnectedOnce) {
    // the connection is already being established
    return;
  }
  connection = createConnection();

  // building absolute path so that websocket doesn't fail when deploying with a context path
  const loc = window.location;
  const baseHref = document.querySelector('base')!.getAttribute('href')!.replace(/\/$/, '');

  const headers = {};
  let url = '//' + loc.host + baseHref + '/websocket';
  const authToken = Storage.local.get('jhi-authenticationToken') || Storage.session.get('jhi-authenticationToken');
  if (authToken) {
    url += '?access_token=' + authToken;
  }
  const socket = new SockJS(url);
  stompClient = Stomp.over(socket, { protocols: ['v12.stomp'] });

  stompClient.connect(headers, () => {
    connectedPromise('success');
    connectedPromise = null;
    sendActivity(window.location.pathname);
    alreadyConnectedOnce = true;
  });
};

const disconnect = () => {
  if (stompClient !== null) {
    if (stompClient.connected) {
      stompClient.disconnect();
    }
    stompClient = null;
  }
  alreadyConnectedOnce = false;
};

const unsubscribe = () => {
  subscriber?.unsubscribe();
};

export default store => next => action => {
  if (getAccount.fulfilled.match(action)) {
    connect();
    const isAdmin = action.payload.data.authorities.includes('ROLE_ADMIN');
    if (!alreadyConnectedOnce) {
      subscribe('/user/queue/heater').then(listener =>
        listener.subscribe(serial => {
          store.dispatch(updateHeater(serial));
        })
      );
    }
  } else if (getAccount.rejected.match(action) || action.type === logoutSession().type) {
    unsubscribe();
    disconnect();
  }
  return next(action);
};
