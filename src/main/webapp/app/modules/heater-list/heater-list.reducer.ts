import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';

const initialState = {
  heaters: [] as Heater[],
};

export type HeaterListState = Readonly<typeof initialState>;

export type Heater = {
  id: number;
  serial: string;
};

const heaterListUrl = '/api/heaters';

export const fetchHeaterList = createAsyncThunk('heaterList.fetch_heater_list', async () => axios.get<Heater[]>(heaterListUrl));

export const HeaterListSlice = createSlice({
  name: 'heaterList',
  initialState: initialState as HeaterListState,
  reducers: {},
  extraReducers(builder) {
    builder.addCase(fetchHeaterList.fulfilled, (state, action) => {
      state.heaters = action.payload.data;
    });
  },
});

export default HeaterListSlice.reducer;
