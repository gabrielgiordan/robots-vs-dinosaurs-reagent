FROM nginx:alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY ./public /var/www

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]