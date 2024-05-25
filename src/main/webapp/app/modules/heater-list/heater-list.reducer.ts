import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';

const initialState = {
  heaters: [] as Heater[],
  countries: [] as Country[],
};

export type HeaterListState = Readonly<typeof initialState>;

export type Heater = {
  id: number;
  name: string;
  serial: string;
  location?: Location;
};

export type Location = {
  country: Country;
  latitude: number;
  longitude: number;
};

export type Country = {
  id: number;
  name: string;
};

export type NewHeaterDTO = {
  name: string;
  serial: string;
  password: string;
  location: LocationDTO;
};

export type LocationDTO = {
  country_id: number;
  latitude: string;
  longitude: string;
};

const heaterListUrl = '/api/heaters';
const addHeaterUrl = '/api/heaters/add';
const countryListUrl = '/api/countries';

export const fetchHeaterList = createAsyncThunk('heaterList.fetch_heater_list', async () => axios.get<Heater[]>(heaterListUrl));

export const fetchCountryList = createAsyncThunk('heaterList.fetch_country_list', async () => axios.get<Country[]>(countryListUrl));

export const addHeater = createAsyncThunk('heaterList.add_heater', async (newHeaterDTO: NewHeaterDTO, { dispatch }) => {
  await axios.post(addHeaterUrl, newHeaterDTO);
  dispatch(fetchHeaterList());
});

export const HeaterListSlice = createSlice({
  name: 'heaterList',
  initialState: initialState as HeaterListState,
  reducers: {},
  extraReducers(builder) {
    builder.addCase(fetchHeaterList.fulfilled, (state, action) => {
      state.heaters = action.payload.data;
    });
    builder.addCase(fetchCountryList.fulfilled, (state, action) => {
      state.countries = action.payload.data;
    });
  },
});

export default HeaterListSlice.reducer;
