import { Component, OnInit } from '@angular/core';

// Adjust these import paths to match your actual project layout.
import { VehicleService } from '../services/vehicle.service';
import { VehicleDto } from '../models/vehicle.model';
import { DataTableColumn } from '../shared/data-table/data-table.component';

/**
 * Citizen's own vehicles — GET /api/vehicles/my.
 */
@Component({
  selector: 'app-vehicle-list',
  standalone: false,
  templateUrl: './vehicle-list.component.html',
  styleUrls: ['./vehicle-list.component.css']
})
export class VehicleListComponent implements OnInit {
  vehicles: VehicleDto[] = [];
  loading = false;
  error = '';

  columns: DataTableColumn<VehicleDto>[] = [
    { key: 'licencePlate', label: 'Plate', mono: true },
    { key: 'make', label: 'Make' },
    { key: 'model', label: 'Model' },
    { key: 'year', label: 'Year', align: 'center' },
    {
      key: 'isStolen',
      label: 'Status',
      align: 'center',
      format: (row) => (row.isStolen ? 'Reported stolen' : row.isActive ? 'Active' : 'Inactive')
    }
  ];

  constructor(private vehicleService: VehicleService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.vehicleService.getMyVehicles().subscribe({
      next: (response) => {
        this.vehicles = response.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load your vehicles.';
        this.loading = false;
      }
    });
  }
}