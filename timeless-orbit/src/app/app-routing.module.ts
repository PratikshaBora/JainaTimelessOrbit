import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'home',
    loadChildren: () => import('./home/home.module').then( m => m.HomePageModule)
  },
  {
    path: 'login',
    loadChildren: () => import('./login/login.module').then( m => m.LoginPageModule)
  },
  {
    path: 'lobby',
    loadChildren: () => import('./lobby/lobby.module').then( m => m.LobbyPageModule)
  },
  {
    path: 'room/:id',
    loadChildren: () => import('./room/room.module').then(m => m.RoomPageModule)
  }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes,
      { preloadingStrategy: PreloadAllModules }
    )
  ],
  exports: [RouterModule]
})
export class AppRoutingModule { }
