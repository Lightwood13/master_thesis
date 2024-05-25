import React, { useEffect, useState } from 'react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { addHeater, Country, fetchCountryList, fetchHeaterList, Heater, NewHeaterDTO } from 'app/modules/heater-list/heater-list.reducer';
import { Link } from 'react-router-dom';
import {
  Card,
  CardContent,
  CardHeader,
  IconButton,
  Typography,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  MenuItem,
  Grid,
  Avatar,
} from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import AddIcon from '@mui/icons-material/Add';
import { getUsersAsAdmin } from 'app/modules/administration/user-management/user-management.reducer';

const HeaterList = () => {
  const heaters: Heater[] = useAppSelector(state => state.heaterList.heaters);
  const countries: Country[] = useAppSelector(state => state.heaterList.countries);

  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(fetchHeaterList());
    dispatch(fetchCountryList());
  }, []);

  const [open, setOpen] = useState(false);
  const [newHeater, setNewHeater] = useState({
    name: '',
    serial: '',
    password: '',
    location: { latitude: '', longitude: '', country_id: 0 },
  }) as [NewHeaterDTO, (newHeater: NewHeaterDTO) => void];

  const handleChange = e => {
    const { name, value } = e.target;
    setNewHeater({
      ...newHeater,
      [name]: value,
    });
  };

  const handleLocationChange = e => {
    const { name, value } = e.target;
    setNewHeater({
      ...newHeater,
      location: {
        ...newHeater.location,
        [name]: value,
      },
    });
  };

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleAddHeater = () => {
    dispatch(addHeater(newHeater));
    setNewHeater({ name: '', serial: '', password: '', location: { latitude: '', longitude: '', country_id: 0 } });
    setOpen(false);
  };

  return (
    <div>
      <Grid container spacing={2}>
        {heaters.map((heater, index) => (
          <Grid item key={index} xs={12} sm={6} md={4}>
            <Link to={heater.serial}>
              <Card>
                <CardHeader
                  avatar={
                    <Avatar>
                      <HomeIcon />
                    </Avatar>
                  }
                  titleTypographyProps={{ variant: 'h4' }}
                  title={heater.name}
                />
                <CardContent>
                  <Typography>Serial: {heater.serial}</Typography>
                  <Typography>Country: {heater.location?.country.name}</Typography>
                  <Typography>Latitude: {heater.location?.latitude}</Typography>
                  <Typography>Longitude: {heater.location?.longitude}</Typography>
                </CardContent>
              </Card>
            </Link>
          </Grid>
        ))}
      </Grid>
      <Button variant="contained" color="primary" startIcon={<AddIcon />} onClick={handleClickOpen} style={{ marginTop: '20px' }}>
        Add New Heater
      </Button>
      <Dialog open={open} onClose={handleClose}>
        <DialogTitle>Add New Heater</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            name="name"
            label="Heater Name"
            type="text"
            fullWidth
            value={newHeater.name}
            onChange={handleChange}
          />
          <TextField margin="dense" name="serial" label="Serial" type="text" fullWidth value={newHeater.serial} onChange={handleChange} />
          <TextField
            margin="dense"
            name="password"
            label="Password"
            type="password"
            fullWidth
            value={newHeater.password}
            onChange={handleChange}
          />
          <Typography variant="h6" gutterBottom>
            Location
          </Typography>
          <TextField
            margin="dense"
            name="latitude"
            label="Latitude"
            type="number"
            fullWidth
            value={newHeater.location.latitude}
            onChange={handleLocationChange}
          />
          <TextField
            margin="dense"
            name="longitude"
            label="Longitude"
            type="number"
            fullWidth
            value={newHeater.location.longitude}
            onChange={handleLocationChange}
          />
          <TextField
            margin="dense"
            name="country_id"
            label="Country"
            select
            fullWidth
            value={newHeater.location.country_id}
            onChange={handleLocationChange}
          >
            {countries.map(option => (
              <MenuItem key={option.id} value={option.id}>
                {option.name}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose} color="primary">
            Cancel
          </Button>
          <Button onClick={handleAddHeater} color="primary">
            Add Heater
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default HeaterList;
