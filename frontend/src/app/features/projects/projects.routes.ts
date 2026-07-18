import { Routes } from '@angular/router';
import { ProjectsListPageComponent } from './projects-list-page.component';
import { ProjectDetailPageComponent } from './project-detail-page.component';

export const PROJECTS_ROUTES: Routes = [
  { path: '', component: ProjectsListPageComponent },
  { path: ':id', component: ProjectDetailPageComponent },
];
