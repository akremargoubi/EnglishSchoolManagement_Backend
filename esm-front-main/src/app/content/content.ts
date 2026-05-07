import { Component } from '@angular/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { bootstrapCheck } from '@ng-icons/bootstrap-icons';

@Component({
  selector: 'app-content',
  standalone: true,
  imports: [NgIconComponent],
  providers: [provideIcons({ bootstrapCheck })],
  templateUrl: './content.html',
  styleUrl: './content.css'
})
export class Content {}
