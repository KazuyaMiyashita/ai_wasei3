import Layout from "./components/Layout";
import { ApplicationProvider } from "./context/ApplicationContext";

function App() {
  return (
    <ApplicationProvider>
      <Layout />
    </ApplicationProvider>
  );
}

export default App;
