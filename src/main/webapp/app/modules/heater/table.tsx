import React, { ChangeEvent, useState } from 'react';
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

const initialData = [
  {
    modelName: 'Test Model',
    targetTemperature: '21°C',
    temperatureRange: '19-23°C',
    dateCreated: '2024-05-16',
    status: 'Gathering training data',
    isActive: true,
  },
  // {
  //   modelName: 'Model B',
  //   targetTemperature: '20°C',
  //   temperatureRange: '18-22°C',
  //   dateCreated: '2024-05-03',
  //   status: 'Inactive',
  // }
];

const ModelTable = () => {
  const [data, setData] = useState(initialData);
  const [open, setOpen] = useState(false);
  const [newModel, setNewModel] = useState({
    modelName: 'Test Model',
    targetTemperature: '21',
    minTemperature: '19',
    maxTemperature: '23',
    isActive: true,
  });

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
    // setData([...data, newModel]);
    setNewModel({
      modelName: '',
      targetTemperature: '',
      minTemperature: '',
      maxTemperature: '',
      isActive: true,
    });
    handleClose();
  };

  return (
    <div className="p-4">
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
            {data.map((row, index) => (
              <TableRow key={index} className={`hover:bg-gray-100 ${row.isActive ? 'bg-green-100' : ''}`}>
                <TableCell>{row.modelName}</TableCell>
                <TableCell>{row.targetTemperature}</TableCell>
                <TableCell>{row.temperatureRange}</TableCell>
                <TableCell>{row.dateCreated}</TableCell>
                <TableCell>{row.status}</TableCell>
                <TableCell>{row.isActive ? 'Active' : 'Inactive'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpen} className="mt-4">
        Create new model
      </Button>

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
                  checked={newModel.isActive}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => {
                    setNewModel({
                      ...newModel,
                      isActive: e.target.checked,
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
