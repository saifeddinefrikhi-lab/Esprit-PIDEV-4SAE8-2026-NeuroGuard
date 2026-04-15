export interface NavigationItem {
  id: string;
  title: string;
  type: 'item' | 'collapse' | 'group';
  translate?: string;
  icon?: string;
  hidden?: boolean;
  url?: string;
  classes?: string;
  groupClasses?: string;
  exactMatch?: boolean;
  external?: boolean;
  target?: boolean;
  breadcrumbs?: boolean;
  children?: NavigationItem[];
  link?: string;
  description?: string;
  path?: string;
}


export const NavigationItems: NavigationItem[] = [
  {
    id: 'dashboard',
    title: 'Dashboard',
    type: 'group',
    icon: 'dashboard',  // Ant Design dashboard icon
    children: [
      {
        id: 'admin-dashboard',
        title: 'Admin Dashboard',
        type: 'item',
        classes: 'nav-item',
        url: '/admin/dashboard',
        icon: 'home', // Ant Design home icon
        breadcrumbs: false
      },
    ]
  },
  {
    id: 'user-management',
    title: 'User Management',
    type: 'group',
    icon: 'user',  // Ant Design user icon
    children: [
      {
        id: 'manage-providers',
        title: 'Users',
        type: 'item',
        url: '/admin/users',
        classes: 'nav-item',
        icon: 'user-add', // Ant Design user-add icon
      },
   
    ]
  },
  {
    id: 'appointment-management',
    title: 'Appointment Management',
    type: 'group',
    icon: 'calendar',  // Ant Design calendar icon
    children: [
      {
        id: 'appointments',
        title: 'Appointments',
        type: 'item',
        classes: 'nav-item',
        url: '/admin/appointments',
        icon: 'calendar', // Ant Design calendar icon
      },
      {
        id: 'consultations',
        title: 'Consultations',
        type: 'item',
        classes: 'nav-item',
        url: '/admin/consultations',
        icon: 'plus-circle', // Ant Design plus-circle icon
      },
    ]
  },
  {
    id: 'healthcare-management',
    title: 'Healthcare Management',
    type: 'group',
    icon: 'health',  // Ant Design health icon
    children: [
      {
        id: 'manage-medications',
        title: 'Manage Medications',
        type: 'item',
        url: '/admin/medications',
        classes: 'nav-item',
        icon: 'experiment', // Ant Design experiment icon (medicine)
      },
      {
        id: 'manage-medical-history',
        title: 'Manage Medical History',
        type: 'item',
        url: '/admin/medical-history',
        classes: 'nav-item',
        icon: 'book', // Ant Design book icon
      },
    ]
  },
  {
    id: 'reports',
    title: 'Reports',
    type: 'group',
    icon: 'file-pdf',  // Ant Design file-pdf icon
    children: [
       {
        id: 'generate-reports',
        title: 'Forums',
        type: 'item',
        url: '/admin/forum', // Path to generate reports
        classes: 'nav-item',
        icon: 'file-text', // Ant Design file-text icon
      },
      {
        id: 'patient-reports',
        title: 'Patient Reports',
        type: 'item',
        url: '/admin/reports/patient',
        classes: 'nav-item',
        icon: 'book', // Ant Design file icon
      },
      {
        id: 'appointment-reports',
        title: 'Appointment Reports',
        type: 'item',
        url: '/admin/reports/appointments',
        classes: 'nav-item',
        icon: 'calendar', // Ant Design calendar icon
      },
    ]
  },
  
];

