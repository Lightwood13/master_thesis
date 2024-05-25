import './home.scss';

import React from 'react';
import { Link } from 'react-router-dom';
import { Translate } from 'react-jhipster';
import { Row, Col, Alert } from 'reactstrap';

import { useAppSelector } from 'app/config/store';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);

  return (
    <Col md="9">
      {account?.login ? (
        <div>
          <Alert color="success">
            <Translate contentKey="home.logged.message" interpolate={{ username: account.login }}>
              You are logged in as user {account.login}.
            </Translate>
          </Alert>

          <Link to="/heaters">Go to your heaters</Link>
        </div>
      ) : (
        <div>
          <Alert color="warning">
            <p>You are not authenticated. Please</p>
            <Link to="/login" className="alert-link">
              <Translate contentKey="global.messages.info.authenticated.link"> sign in</Translate>
            </Link>
            <p>or</p>
            <Link to="/account/register" className="alert-link">
              <Translate contentKey="global.messages.info.register.link">register a new account</Translate>
            </Link>
          </Alert>
        </div>
      )}
    </Col>
  );
};

export default Home;
