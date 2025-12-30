import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { RouteReuseStrategy } from '@angular/router';

import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { SocketIoModule, SocketIoConfig } from 'ngx-socket-io';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { LobbyPage } from './lobby/lobby.page';
import { FormsModule } from '@angular/forms';
import { ScoreboardComponent } from './scoreboard/scoreboard.component';
import { HttpClientModule } from '@angular/common/http';


@NgModule({
  declarations: [
                  AppComponent,
                  ScoreboardComponent
                ],
  imports: [
    BrowserModule,
    IonicModule.forRoot(),
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    // SocketIoModule.forRoot(config)
],
  providers: [{ provide: RouteReuseStrategy, useClass: IonicRouteStrategy }],
  bootstrap: [AppComponent],
})
export class AppModule {}
