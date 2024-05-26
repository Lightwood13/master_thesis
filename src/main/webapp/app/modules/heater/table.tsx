import React, { ChangeEvent, ReactElement, useState } from 'react';
import {
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Modal,
  Box,
  TextField,
  Checkbox,
  FormControlLabel,
  FormGroup,
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { createNewModel, Model } from 'app/modules/heater/heater.reducer';

const style = {
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 400,
  bgcolor: 'background.paper',
  boxShadow: 24,
  p: 4,
};

const ModelTable = () => {
  const [open, setOpen] = useState(false);
  const [newModel, setNewModel] = useState({
    modelName: 'Test Model',
    targetTemperature: '21',
    minTemperature: '19',
    maxTemperature: '23',
    activateImmediately: true,
  });

  const heater = useAppSelector(state => state.heater.heater);
  const models = useAppSelector(state => state.heater.models);
  const dispatch = useAppDispatch();

  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const handleChange = e => {
    const { name, value } = e.target;
    setNewModel({
      ...newModel,
      [name]: value,
    });
  };

  const handleSubmit = () => {
    setNewModel({
      modelName: 'Test Model',
      targetTemperature: '21',
      minTemperature: '19',
      maxTemperature: '23',
      activateImmediately: true,
    });
    if (heater) {
      dispatch(
        createNewModel({
          serial: heater.serial,
          data: {
            name: newModel.modelName,
            targetTemperature: parseFloat(newModel.targetTemperature),
            minTemperature: parseFloat(newModel.minTemperature),
            maxTemperature: parseFloat(newModel.maxTemperature),
            activateImmediately: newModel.activateImmediately,
          },
        })
      );
    }
    handleClose();
  };

  let table: ReactElement;
  if (models.length === 0) {
    table = <p>No models created</p>;
  } else {
    table = (
      <TableContainer component={Paper}>
        <Table>
          <TableHead className="bg-gray-200">
            <TableRow>
              <TableCell className="font-bold">Model Name</TableCell>
              <TableCell className="font-bold">Target Temperature</TableCell>
              <TableCell className="font-bold">Temperature Range</TableCell>
              <TableCell className="font-bold">Date Created</TableCell>
              <TableCell className="font-bold">Status</TableCell>
              <TableCell className="font-bold">Active</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {models.map((model: Model, index) => {
              const isActive = model.id === heater?.activeModelId;
              return (
                <TableRow key={index} className={`hover:bg-gray-100 ${isActive ? 'bg-green-100' : ''}`}>
                  <TableCell>{model.name}</TableCell>
                  <TableCell>{model.targetTemperature}°C</TableCell>
                  <TableCell>
                    {model.minTemperature}-{model.maxTemperature}°C
                  </TableCell>
                  <TableCell>{model.createdOn.toFormat('dd MMM yyyy')}</TableCell>
                  <TableCell>{model.status}</TableCell>
                  <TableCell>{isActive ? 'Active' : 'Inactive'}</TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    );
  }

  let createNewModelButton: ReactElement;
  if (heater?.calibrationStatus !== 'CALIBRATED') {
    createNewModelButton = (
      <div className="space-y-4">
        <p>To create models please calibrate the heater</p>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpen} className="mt-4" disabled>
          Create new model
        </Button>
      </div>
    );
  } else {
    createNewModelButton = (
      <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpen} className="mt-4">
        Create new model
      </Button>
    );
  }

  return (
    <div className="p-4">
      {table}
      {createNewModelButton}

      <Modal open={open} onClose={handleClose}>
        <Box sx={style}>
          <h2 className="mb-4">Create New Model</h2>
          <TextField label="Model Name" name="modelName" value={newModel.modelName} onChange={handleChange} fullWidth className="mb-4" />
          <TextField
            label="Target Temperature"
            name="targetTemperature"
            value={newModel.targetTemperature}
            onChange={handleChange}
            fullWidth
            className="mb-4"
          />
          <div className="flex space-x-4">
            <TextField
              label="Minimum Temperature"
              name="minTemperature"
              value={newModel.minTemperature}
              onChange={handleChange}
              fullWidth
              className="mb-4"
            />
            <TextField
              label="Maximum Temperature"
              name="maxTemperature"
              value={newModel.maxTemperature}
              onChange={handleChange}
              fullWidth
              className="mb-4"
            />
          </div>
          <FormGroup className="mb-4">
            <FormControlLabel
              label="Activate immediately"
              control={
                <Checkbox
                  name="active"
                  checked={newModel.activateImmediately}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => {
                    setNewModel({
                      ...newModel,
                      activateImmediately: e.target.checked,
                    });
                  }}
                />
              }
            />
          </FormGroup>
          <Button variant="contained" onClick={handleSubmit}>
            Create
          </Button>
        </Box>
      </Modal>
    </div>
  );
};

export default ModelTable;
